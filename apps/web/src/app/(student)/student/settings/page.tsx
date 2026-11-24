import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import ProfileSection from '@/components/student/settings/ProfileSection'
import PasswordSection from '@/components/student/settings/PasswordSection'
import PreferencesSection from '@/components/student/settings/PreferencesSection'
import TransactionsSection from '@/components/student/settings/TransactionsSection'
import { User, Shield, Sliders, Receipt } from 'lucide-react'
import { getTranslations } from 'next-intl/server'

export default async function SettingsPage() {
  const ui = await getTranslations('privateUi')
  return (
    <div className='flex flex-col gap-6 max-w-4xl'>
      <div className='flex flex-col gap-1'>
        <h1 className='text-2xl font-heading font-medium text-[#1c3873]'>{ui('settings')}</h1>
        <p className='text-sm text-[#5e5f5c]'>{ui('manageAccountPreferencesBilling')}</p>
      </div>

      <Tabs defaultValue="profile" className="w-full flex flex-col gap-6">
        <TabsList className="bg-transparent border-b border-gray-200 p-0 rounded-none h-auto w-full justify-start overflow-x-auto flex-nowrap [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
          <TabsTrigger value="profile" className="cursor-pointer data-[state=active]:bg-transparent data-[state=active]:text-[#1c3873] data-[state=active]:border-[#1c3873] data-[state=active]:shadow-none hover:text-[#1c3873] text-[#5e5f5c] border-b-2 border-transparent rounded-none px-4 py-3 text-sm font-medium transition-all duration-300 flex items-center gap-2 whitespace-nowrap">
            <User className="w-4 h-4" />
            {ui('profileInfo')}
          </TabsTrigger>
          <TabsTrigger value="security" className="cursor-pointer data-[state=active]:bg-transparent data-[state=active]:text-[#1c3873] data-[state=active]:border-[#1c3873] data-[state=active]:shadow-none hover:text-[#1c3873] text-[#5e5f5c] border-b-2 border-transparent rounded-none px-4 py-3 text-sm font-medium transition-all duration-300 flex items-center gap-2 whitespace-nowrap">
            <Shield className="w-4 h-4" />
            {ui('security')}
          </TabsTrigger>
          <TabsTrigger value="preferences" className="cursor-pointer data-[state=active]:bg-transparent data-[state=active]:text-[#1c3873] data-[state=active]:border-[#1c3873] data-[state=active]:shadow-none hover:text-[#1c3873] text-[#5e5f5c] border-b-2 border-transparent rounded-none px-4 py-3 text-sm font-medium transition-all duration-300 flex items-center gap-2 whitespace-nowrap">
            <Sliders className="w-4 h-4" />
            {ui('preferences')}
          </TabsTrigger>
          <TabsTrigger value="transactions" className="cursor-pointer data-[state=active]:bg-transparent data-[state=active]:text-[#1c3873] data-[state=active]:border-[#1c3873] data-[state=active]:shadow-none hover:text-[#1c3873] text-[#5e5f5c] border-b-2 border-transparent rounded-none px-4 py-3 text-sm font-medium transition-all duration-300 flex items-center gap-2 whitespace-nowrap">
            <Receipt className="w-4 h-4" />
            {ui('transactions')}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="profile" className="m-0 border-none p-0 outline-none">
          <ProfileSection />
        </TabsContent>
        
        <TabsContent value="security" className="m-0 border-none p-0 outline-none">
          <PasswordSection />
        </TabsContent>
        
        <TabsContent value="preferences" className="m-0 border-none p-0 outline-none">
          <PreferencesSection />
        </TabsContent>
        
        <TabsContent value="transactions" className="m-0 border-none p-0 outline-none">
          <TransactionsSection />
        </TabsContent>
      </Tabs>
    </div>
  )
}
